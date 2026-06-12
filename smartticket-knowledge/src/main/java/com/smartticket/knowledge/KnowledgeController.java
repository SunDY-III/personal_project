package com.smartticket.knowledge;

import com.smartticket.cache.CacheService;
import com.smartticket.common.R;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.DigestUtils;
import java.time.Duration;
import java.util.*;

@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {
    private final DocumentParseService parseService;
    private final EmbeddingService embeddingService;
    private final CacheService cache;
    private final JdbcTemplate jdbc;

    @PostMapping("/documents")
    public R<?> upload(@RequestParam("file") MultipartFile file, HttpServletRequest req) throws Exception {
        Long userId = (Long) req.getAttribute("userId");
        // 文件类型校验
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.matches(".*\\.(pdf|docx|txt|md)$"))
            throw new com.smartticket.common.BizException(400, "仅支持 PDF/DOCX/TXT/MD 格式");
        if (file.getSize() > 10 * 1024 * 1024)
            throw new com.smartticket.common.BizException(400, "文件大小不能超过 10MB");
        String md5 = org.springframework.util.DigestUtils.md5DigestAsHex(file.getInputStream());
        return R.ok(parseService.upload(file.getOriginalFilename(), file.getInputStream(), md5, userId));
    }

    @GetMapping("/documents")
    public R<?> list() { return R.ok(parseService.list()); }

    @PostMapping("/ask")
    public R<?> ask(@RequestBody Map<String, String> body) {
        String question = body.get("question");
        // 检查缓存
        String hash = cache.questionHash(question);
        String cached = cache.get("rag:qa:" + hash);
        if (cached != null) return R.ok(Map.of("answer", cached, "cached", true));
        // 向量检索
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
