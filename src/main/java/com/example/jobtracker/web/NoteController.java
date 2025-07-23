package com.example.jobtracker.web;

import com.example.jobtracker.dto.NoteDto;
import com.example.jobtracker.service.NoteService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.data.domain.Page;



import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import com.example.jobtracker.domain.Note;
import com.example.jobtracker.repository.NoteRepository;



@RestController
@RequestMapping("/api/notes")
public class NoteController {
    private final NoteService service;

    public NoteController(NoteService service) {
        this.service = service;
    }

    @PostMapping
    public void create(@RequestBody NoteDto dto) {
        service.save(dto);
    }

    @GetMapping("/{jobId}")
    public List<NoteDto> getNotes(@PathVariable Long jobId) {
        return service.findByJobId(jobId);
    }
    @GetMapping("/{jobId}/paged")
    public Page<NoteDto> getPagedNotes(@PathVariable Long jobId,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "5") int size) {
        return service.findByJobIdPaged(jobId, page, size);
    }
    @PostMapping("/batch")
    public void batchCreate(@RequestBody List<NoteDto> notes) {
        service.saveAll(notes);
    }
    @DeleteMapping("/{id}")
    public void deleteNote(@PathVariable Long id) {
        service.delete(id);
    }
        @PostMapping("/uploadWithNote")
        public String uploadWithNote(@RequestParam("file") MultipartFile file,
                @RequestParam("jobId") Long jobId,
                @RequestParam("content") String content) throws IOException {

            if (file.isEmpty()) {
                return "文件不能为空";
            }

            String contentType = file.getContentType();
            if (!contentType.equals("application/pdf")
                    && !contentType.startsWith("image/")
                    && !contentType.startsWith("audio/")) {
                return "只支持 PDF / 图片 / 音频";
            }

            String projectPath = System.getProperty("user.dir"); // 获取项目根目录
            String path = projectPath + "/uploads/notes/" + file.getOriginalFilename();//构建上传路径
            File dest = new File(path);//保存文件
            file.transferTo(dest);


            Note note = new Note();
            note.setJobId(jobId);
            note.setContent(content);
            note.setFilePath(path);
            note.setCreatedAt(LocalDateTime.now());

           service.save(note);

            return "笔记和文件上传成功：" +path;
        }
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam("file") String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName())
                .contentLength(file.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }



}
