package com.example.huarongdao.user;

/** users 表实体。passwordHash 只在服务端使用，不会返回给浏览器。 */
public record UserAccount(long id, String username, String passwordHash) {
}
