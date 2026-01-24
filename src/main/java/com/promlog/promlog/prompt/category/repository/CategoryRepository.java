package com.promlog.promlog.prompt.category.repository;

import com.promlog.promlog.prompt.category.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // ✅ 프롬프트 생성/수정에서 사용 (삭제된 카테고리 제외)
    List<Category> findByIdInAndDeletedAtIsNull(Collection<Long> ids);

    // ✅ 카테고리 목록 API에서 사용 (삭제된 카테고리 제외)
    List<Category> findByDeletedAtIsNullOrderByNameAsc();
}
