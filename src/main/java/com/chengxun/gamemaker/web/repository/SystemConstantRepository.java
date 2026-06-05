package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.SystemConstant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemConstantRepository extends JpaRepository<SystemConstant, Long> {

    Optional<SystemConstant> findByConstantKey(String constantKey);

    List<SystemConstant> findByGroupNameOrderByDisplayNameAsc(String groupName);

    List<SystemConstant> findAllByOrderByGroupNameAscDisplayNameAsc();

    boolean existsByConstantKey(String constantKey);
}
