package com.pohanghang.spotline.domain.store.repository;

import com.pohanghang.spotline.domain.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, Long> {
}
