package org.hgc.suts.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.UUID;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;

import org.hgc.suts.user.common.constant.RedisCacheConstant;
import org.hgc.suts.user.common.errorcode.BaseErrorCode;
import org.hgc.suts.user.common.exception.ClientException;
import org.hgc.suts.user.dao.entity.UserDO;
import org.hgc.suts.user.dao.mapper.UserMapper;
import org.hgc.suts.user.dto.req.UserLoginReqDTO;
import org.hgc.suts.user.dto.req.UserRegisterReqDTO;
import org.hgc.suts.user.dto.resp.UserLoginRespDTO;
import org.hgc.suts.user.dto.resp.UserRespDTO;
import org.hgc.suts.user.service.UserService;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.hgc.suts.user.common.constant.RedisCacheConstant.LOCK_USER_REGISTER_KEY;
import static org.hgc.suts.user.common.constant.RedisCacheConstant.USER_LOGIN_KEY;

/**
* @author 谢毅强
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-11-27 22:52:37
*/
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {

    private final RBloomFilter<String> userRegisterCachePenetrationBloomFilter;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void register(UserRegisterReqDTO requestParam) {
        // 验证布隆过滤器中是否存在该账号
        if (hasUserAccount(requestParam.getUserAccount())) {
            throw new ClientException(BaseErrorCode.USER_ACCOUNT_EXIST_ERROR);
        }
        RLock lock = redissonClient.getLock(RedisCacheConstant.LOCK_USER_REGISTER_KEY + requestParam.getUserAccount());
        if (!lock.tryLock()) {
            throw new ClientException(BaseErrorCode.USER_ACCOUNT_EXIST_ERROR);
        }
        try {
            int inserted = baseMapper.insert(BeanUtil.toBean(requestParam, UserDO.class));
            if (inserted < 1) {
                throw new ClientException(BaseErrorCode.USER_SAVE_ERROR);
            }
            userRegisterCachePenetrationBloomFilter.add(requestParam.getUserAccount());
        } catch (DuplicateKeyException ex) {
            throw new ClientException(BaseErrorCode.USER_ACCOUNT_EXIST_ERROR);
        } finally {
            lock.unlock();
        }
    }


    @Override
    public Boolean hasUserAccount(String username) {
        return userRegisterCachePenetrationBloomFilter.contains(username);
    }

    @Override
    public UserLoginRespDTO login(UserLoginReqDTO requestParam) {
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUserAccount, requestParam.getUserAccount())
                .eq(UserDO::getUserPassword, requestParam.getPassword())
                .eq(UserDO::getIsDelete, 0);
        UserDO userDO = baseMapper.selectOne(queryWrapper);
        Map<Object, Object> hasLoginMap = stringRedisTemplate.opsForHash().entries(RedisCacheConstant.USER_LOGIN_KEY + requestParam.getUserAccount());
        if (CollUtil.isNotEmpty(hasLoginMap)) {
            stringRedisTemplate.expire(RedisCacheConstant.USER_LOGIN_KEY + requestParam.getUserAccount(), 30L, TimeUnit.MINUTES);
            String token = hasLoginMap.keySet().stream()
                    .findFirst()
                    .map(Object::toString)
                    .orElseThrow(() -> new ClientException("用户登录错误"));
            return new UserLoginRespDTO(token);
        }

        /*
          Hash
          Key：login_用户名
          Value：
           Key：token标识
           Val：JSON 字符串（用户信息）
         */
        String uuid = UUID.randomUUID().toString();
        // 返回脱敏用户信息
        UserRespDTO userRespDTO = BeanUtil.toBean(userDO, UserRespDTO.class);
        stringRedisTemplate.opsForHash().put(RedisCacheConstant.USER_LOGIN_KEY + requestParam.getUserAccount(), uuid, JSON.toJSONString(userRespDTO));
        stringRedisTemplate.expire(RedisCacheConstant.USER_LOGIN_KEY + requestParam.getUserAccount(), 30L, TimeUnit.MINUTES);
        return new UserLoginRespDTO(uuid);
    }

}




