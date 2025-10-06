package com.guochang.interviewpracticebackend.constant;

public interface RedisConstant {


   String USER_SIGN_IN_REDIS_KEY_PREFIX="user:signins";

   static String getUserSignInRedisKey(Integer year,long userId){
        return String.format("%s:%s:%s",USER_SIGN_IN_REDIS_KEY_PREFIX,year,userId);
   }









}
