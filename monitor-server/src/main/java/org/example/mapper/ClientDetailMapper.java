package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.entity.dto.Client;
import org.example.entity.dto.ClientDetail;

@Mapper
public interface ClientDetailMapper extends BaseMapper<ClientDetail> {
}
