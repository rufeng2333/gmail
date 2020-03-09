package com.rufeng2333.gmall.service;

import com.rufeng2333.gmall.bean.UmsMember;
import com.rufeng2333.gmall.bean.UmsMemberReceiveAddress;

import java.util.List;

public interface UserService {
    List<UmsMember> findAll();

    List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId);
}
