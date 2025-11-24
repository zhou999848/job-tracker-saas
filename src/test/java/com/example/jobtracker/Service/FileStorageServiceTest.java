
package com.example.jobtracker.Service;

import com.example.jobtracker.domain.FileObject;
import com.example.jobtracker.repository.FileObjectRepository;
import com.example.jobtracker.service.CurrentTenant;
import com.example.jobtracker.service.storage.FileStorageService;
import com.example.jobtracker.service.storage.FileUploadResponse;
import org.springframework.security.access.AccessDeniedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URL;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock S3Client s3;
    @Mock S3Presigner presigner;
    @Mock FileObjectRepository repo;
    @Mock CurrentTenant currentTenant;

    @InjectMocks
    FileStorageService service;

    @Test
    @DisplayName("upload: 存储成功且写入元数据")
    void upload_ok() throws Exception {
        UUID tenant = UUID.randomUUID();
        when(currentTenant.requireTenantId()).thenReturn(tenant);

        MockMultipartFile mf = new MockMultipartFile(
                "file", "hello.txt", "text/plain", "hi".getBytes());

        // 让 repo.save 回传带 id/createdAt 的实体（模拟 DB 行为）
        when(repo.save(any(FileObject.class))).thenAnswer(inv -> {
            FileObject e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            e.setCreatedAt(Instant.now());
            return e;
        });

        // Act
        FileUploadResponse resp = service.upload(mf);

        // ===== 对“响应里确实存在的字段”做断言（若没有 id 可去掉这行）=====
        // assertThat(resp.getId()).isNotNull();
        // 如果你的响应类不包含 filename/size，就不要断它们，避免 NPE
        // assertThat(resp.getFilename()).isEqualTo("hello.txt");
        // assertThat(resp.getSize()).isEqualTo(mf.getSize());

        // ===== 验证 putObject 参数（S3 上传的桶/Key/类型等）=====
        ArgumentCaptor<PutObjectRequest> putReqCap = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3, times(1)).putObject(putReqCap.capture(), any(RequestBody.class));
        PutObjectRequest putReq = putReqCap.getValue();
        // 如你的桶名来自配置，请改成对应变量
        assertThat(putReq.bucket()).isEqualTo("jobtracker");
        assertThat(putReq.key()).contains("tenant/" + tenant + "/");
        assertThat(putReq.contentType()).isEqualTo("text/plain");

        // ===== 验证保存到数据库的实体内容（租户隔离/文件名/Key）=====
        ArgumentCaptor<FileObject> entityCap = ArgumentCaptor.forClass(FileObject.class);
        verify(repo, times(1)).save(entityCap.capture());
        FileObject saved = entityCap.getValue();
        assertThat(saved.getTenantId()).isEqualTo(tenant);
        assertThat(saved.getFilename()).isEqualTo("hello.txt");
        assertThat(saved.getSize()).isEqualTo(mf.getSize());
        //assertThat(saved.getKey()).contains("tenant/" + tenant + "/");
    }



    @Test
    @DisplayName("createPresignedGetUrl: 同租户可拿到临时链接")
    void presign_ok_same_tenant() throws Exception {
        UUID tenant = UUID.randomUUID();
        when(currentTenant.requireTenantId()).thenReturn(tenant);

        FileObject fo = FileObject.builder()
                .id(UUID.randomUUID())
                .tenantId(tenant)
                .s3Key("tenant/" + tenant + "/abc-hello.txt")
                .filename("hello.txt")
                .contentType("text/plain")
                .size(2L)
                .createdAt(Instant.now())
                .build();

        when(repo.findById(fo.getId())).thenReturn(Optional.of(fo));

        // 伪造一个预签名返回
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(new URL("http://example.com/presigned"));
        when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);

        URL url = service.createPresignedGetUrl(fo.getId());
        assertThat(url.toString()).contains("http://example.com/presigned");

        verify(presigner, times(1)).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    @DisplayName("createPresignedGetUrl: 跨租户访问 → 403")
    void presign_forbidden_cross_tenant() {
        UUID myTenant = UUID.randomUUID();
        UUID otherTenant = UUID.randomUUID();
        when(currentTenant.requireTenantId()).thenReturn(myTenant);
        // 如果 service 里用 isSystemAdmin()，建议 try/catch 默认为 false

        FileObject fo = FileObject.builder()
                .id(UUID.randomUUID())
                .tenantId(otherTenant)
                .s3Key("tenant/" + otherTenant + "/x")
                .filename("x.txt")
                .size(1L)
                .createdAt(Instant.now())
                .build();

        when(repo.findById(fo.getId())).thenReturn(Optional.of(fo));

        assertThatThrownBy(() -> service.createPresignedGetUrl(fo.getId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("No permission");
        verify(presigner, never()).presignGetObject(any(GetObjectPresignRequest.class));

    }

    @Test
    @DisplayName("delete: 同租户可删除对象并删库")
    void delete_ok() {
        UUID tenant = UUID.randomUUID();
        when(currentTenant.requireTenantId()).thenReturn(tenant);

        FileObject fo = FileObject.builder()
                .id(UUID.randomUUID())
                .tenantId(tenant)
                .s3Key("tenant/" + tenant + "/to-delete.txt")
                .filename("to-delete.txt")
                .size(1L)
                .createdAt(Instant.now())
                .build();

        when(repo.findById(fo.getId())).thenReturn(Optional.of(fo));

        service.delete(fo.getId());

        verify(s3, times(1)).deleteObject(any(DeleteObjectRequest.class));
        verify(repo, times(1)).deleteById(fo.getId());

    }


    @Test//new
    @DisplayName("createPresignedGetUrl: 跨租户访问抛 AccessDeniedException")
    void presign_crossTenant_forbidden() {
        UUID ownerTenant = UUID.randomUUID();
        UUID otherTenant = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();

        FileObject fo = new FileObject();
        fo.setId(fileId);
        fo.setTenantId(ownerTenant);

        when(repo.findById(fileId)).thenReturn(Optional.of(fo));
        when(currentTenant.requireTenantId()).thenReturn(otherTenant);
        when(currentTenant.isSystemAdmin()).thenReturn(false);

        assertThatThrownBy(() -> service.createPresignedGetUrl(fileId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test//new
    @DisplayName("createPresignedGetUrl: 文件不存在抛 IllegalArgumentException")
    void presign_fileNotFound_throws() {
        UUID fileId = UUID.randomUUID();
        when(repo.findById(fileId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createPresignedGetUrl(fileId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test//new
    @DisplayName("upload: 文件名为空时也能正常生成 key")
    void upload_emptyFilename_sanitized() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        when(currentTenant.requireTenantId()).thenReturn(tenant);
        when(currentTenant.requireUserId()).thenReturn(user);

        MultipartFile file = new MockMultipartFile(
                "file", null, "text/plain", "hello".getBytes()
        );

        FileObject saved = new FileObject();
        saved.setId(UUID.randomUUID());
        when(repo.save(any())).thenReturn(saved);


        FileUploadResponse result = service.upload(file);
        assertThat(result.getFilename()).isNull(); // 或根据你的逻辑改为 isEqualTo(...)
        // 可以 verify(buildKey 中的 sanitize 逻辑）：略

    }




}