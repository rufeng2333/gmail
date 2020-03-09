package com.rufeng2333.gmall.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.rufeng2333.gmall.bean.UmsMember;
import com.rufeng2333.gmall.bean.UmsMemberReceiveAddress;
import com.rufeng2333.gmall.service.UserService;
import com.rufeng2333.gmall.user.mapper.UmsMemberReceiveAddressMapper;
import com.rufeng2333.gmall.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserMapper userMapper;

    @Autowired
    UmsMemberReceiveAddressMapper umsMemberReceiveAddressMapper;

    @Override
    public List<UmsMember> findAll() {
        List<UmsMember> umsMemberList = userMapper.selectAll();
        return umsMemberList;
    }

    @Override
    public List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId) {
        Example e = new Example(UmsMemberReceiveAddress.class);
        e.createCriteria().andEqualTo("memberId",memberId);
        List<UmsMemberReceiveAddress> umsMemberReceiveAddresses = umsMemberReceiveAddressMapper.selectByExample(e);
        return umsMemberReceiveAddresses;
    }
}
