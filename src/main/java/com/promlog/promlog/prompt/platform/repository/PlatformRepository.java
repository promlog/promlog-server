package com.promlog.promlog.prompt.platform.repository;

import com.promlog.promlog.prompt.platform.domain.Platform;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PlatformRepository extends JpaRepository<Platform, Long> {

    // ✅ 프롬프트 생성/수정에서 사용 (삭제된 플랫폼 제외)
    List<Platform> findByIdInAndDeletedAtIsNull(Collection<Long> ids);

    // ✅ 플랫폼 목록 API에서 사용 (삭제된 플랫폼 제외)
    List<Platform> findByDeletedAtIsNullOrderByNameAsc();
}
