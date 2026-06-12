package com.smartticket.knowledge;

import com.smartticket.cache.CacheService;
import com.smartticket.common.BizException;
import com.smartticket.common.R;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.tika.Tika;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.*;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {
    private final DocumentParseService parseService;
    private final EmbeddingService embeddingService;
    private final CacheService cache;
    private final JdbcTemplate jdbc;
    private final Tika tika = new Tika();

    public KnowledgeController(DocumentParseService parseService, EmbeddingService embeddingService, CacheService cache, JdbcTemplate jdbc) {
        this.parseService = parseService;
        this.embeddingService = embeddingService;
        this.cache = cache;
        this.jdbc = jdbc;
    }

    @PostMapping("/documents")
    public R<?> upload(@RequestParam("file") MultipartFile file, HttpServletRequest req) throws Exception {
        Long userId = (Long) req.getAttribute("userId");
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.matches(".*\\.(pdf|docx|txt|md)$"))
            throw new BizException(400, "仅支持 PDF/DOCX/TXT/MD 格式");
        if (file.getSize() > 10 * 1024 * 1024)
            throw new BizException(400, "文件大小不能超过 10MB");

        // 校验真实 MIME 类型
        String detectedType = tika.detect(file.getInputStream());
        if (!detectedType.matches("(application/pdf|application/vnd\\.openxmlformats.*|text/plain|text/markdown|text/x-markdown)"))
            throw new BizException(400, "文件类型校验失败，仅支持 PDF/DOCX/TXT/MD");

        byte[] fileBytes = file.getBytes();
        String md5 = DigestUtils.md5DigestAsHex(new ByteArrayInputStream(fileBytes));
        return R.ok(parseService.upload(filename, new ByteArrayInputStream(fileBytes), md5, userId));
    }

    @GetMapping("/documents")
    public R<?> list() { return R.ok(parseService.list()); }

    @PostMapping("/ask")
    public R<?> ask(@RequestBody Map<String, String> body) {
        String question = body.get("question");
        String hash = cache.questionHash(question);
        String cached = cache.get("rag:qa:" + hash);
        if (cached != null) return R.ok(Map.of("answer", cached, "cached", true));
        var results = embeddingService.search(question, 5);
        if (results.isEmpty()) return R.ok(Map.of("answer", "当前知识库无法确认该问题", "sources", List.of()));
        StringBuilder context = new StringBuilder();
        List<String> sources = new ArrayList<>();
        for (var r : results) {
            context.append(r.get("content")).append("\n");
            sources.add("doc#" + r.get("doc_id") + " chunk#" + r.get("chunk_id"));
        }
        String snippet = results.get(0).get("content").toString();
        String answer = "根据知识库检索结果：" + (snippet.length() > 100 ? snippet.substring(0, 100) + "..." : snippet);
        cache.set("rag:qa:" + hash, answer, Duration.ofMinutes(30));
        return R.ok(Map.of("answer", answer, "sources", sources));
    }
}
