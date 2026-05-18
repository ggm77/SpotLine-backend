package com.pohanghang.spotline.domain.video.repository;

import com.pohanghang.spotline.domain.video.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoRepository extends JpaRepository<Video, Long> {
}
