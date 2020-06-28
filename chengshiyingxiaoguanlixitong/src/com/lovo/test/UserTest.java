package com.lovo.test;

import com.lovo.dao.UserMapper;
import com.lovo.vo.Sec_User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:spring-mybatis.xml","classpath:spring-servlet.xml"})
public class UserTest {

    @Autowired
    UserMapper userMapper;

    @Test
    public void test1(){
        Sec_User user = new Sec_User();
        user.setPassword("root");
        user.setName("root");
        Sec_User user1 = userMapper.findUserByNameAndPsd(user);
        System.out.println(user1.getUserid());

    }
}
