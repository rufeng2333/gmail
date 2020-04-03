package com.rufeng2333.gmall.service;

import com.rufeng2333.gmall.bean.UmsMember;
import com.rufeng2333.gmall.bean.UmsMemberReceiveAddress;

import java.util.List;

public interface UserService {
    List<UmsMember> findAll();

    List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId);

    UmsMember login(UmsMember umsMember);

    void addUserToken(String token, String memberId);

    UmsMember addOauthUser(UmsMember umsMember);

    UmsMember checkOauthUser(UmsMember umsCheck);

    UmsMemberReceiveAddress getReceiveAddressById(String receiveAddressId);
}
