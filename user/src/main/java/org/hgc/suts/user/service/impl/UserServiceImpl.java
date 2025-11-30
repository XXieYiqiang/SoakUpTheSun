package org.hgc.suts.user.service.impl;

import cn.hutool.Hutool;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.UUID;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import lombok.Value;
import org.hgc.suts.user.common.biz.user.UserContext;
import org.hgc.suts.user.common.constant.RedisCacheConstant;
import org.hgc.suts.user.common.enums.UserErrorCodeEnum;
import org.hgc.suts.user.common.errorcode.BaseErrorCode;
import org.hgc.suts.user.common.exception.ClientException;
import org.hgc.suts.user.common.exception.ServiceException;
import org.hgc.suts.user.dao.entity.UserDO;
import org.hgc.suts.user.dao.mapper.UserMapper;
import org.hgc.suts.user.dto.req.UserLoginReqDTO;
import org.hgc.suts.user.dto.req.UserRegisterReqDTO;
import org.hgc.suts.user.dto.req.UserUpdateReqDTO;
import org.hgc.suts.user.dto.resp.UserLoginRespDTO;
import org.hgc.suts.user.dto.resp.UserRespDTO;
import org.hgc.suts.user.service.UserService;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.hgc.suts.user.common.constant.RedisCacheConstant.USER_LOGIN_KEY_TOKEN_TO_USER;
import static org.hgc.suts.user.common.constant.RedisCacheConstant.USER_LOGIN_KEY_USER_TO_TOKEN;
import static org.hgc.suts.user.common.enums.UserErrorCodeEnum.USER_NULL;

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
            LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class).eq(UserDO::getUserAccount, requestParam.getUserAccount());
            long count = baseMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new ClientException("用户已存在");
            }
            UserDO userDO = BeanUtil.toBean(requestParam, UserDO.class);
            userDO.setUserPassword(getEncryptPassword(requestParam.getUserPassword()));
            baseMapper.insert(userDO);
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
        if (!hasUserAccount(requestParam.getUserAccount())) {
            throw new ClientException("该用户不存在");
        }
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUserAccount, requestParam.getUserAccount())
                .eq(UserDO::getUserPassword, getEncryptPassword(requestParam.getPassword()))
                .eq(UserDO::getIsDelete, 0);
        UserDO userDO = baseMapper.selectOne(queryWrapper);

        if (userDO == null) {
            throw new ClientException("该用户不存在");
        }

        Map<Object, Object> hasLoginMap = stringRedisTemplate.opsForHash().entries(USER_LOGIN_KEY_USER_TO_TOKEN + requestParam.getUserAccount());
        // 脱敏的用户信息
        UserRespDTO userRespDTO = BeanUtil.toBean(userDO, UserRespDTO.class);
        // 构造token
        String token = UUID.randomUUID().toString();
        // token数量>=3时

        // todo 修改为lua脚本
        if (hasLoginMap.size() >= 3){
            // 1.删除一个token,通过token-userAccount的映射去找剩余时间最短的删除
            String tokenToDelete = findTokenWithMinimalTTL(hasLoginMap.keySet(), USER_LOGIN_KEY_TOKEN_TO_USER);
            if (tokenToDelete != null) {
                stringRedisTemplate.opsForHash().delete(USER_LOGIN_KEY_USER_TO_TOKEN + requestParam.getUserAccount(), tokenToDelete);
                }
            // 2.删除token-userAccount映射
            stringRedisTemplate.delete(USER_LOGIN_KEY_TOKEN_TO_USER + tokenToDelete);
        }
        // 添加token
        stringRedisTemplate.opsForHash().put(USER_LOGIN_KEY_USER_TO_TOKEN + requestParam.getUserAccount(), token, JSON.toJSONString(userRespDTO));
        stringRedisTemplate.expire(USER_LOGIN_KEY_USER_TO_TOKEN + requestParam.getUserAccount(), 30L, TimeUnit.HOURS);
        // 以及建立token-userAccount的映射
        stringRedisTemplate.opsForValue().set(USER_LOGIN_KEY_TOKEN_TO_USER + token, requestParam.getUserAccount(),30L, TimeUnit.HOURS);

        return new UserLoginRespDTO(token);
    }

    @Override
    public UserRespDTO getUserByUsername(String userAccount) {
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUserAccount, userAccount);
        UserDO userDO = baseMapper.selectOne(queryWrapper);
        if (userDO == null) {
            throw new ServiceException(USER_NULL);
        }
        UserRespDTO result = new UserRespDTO();
        BeanUtils.copyProperties(userDO, result);
        return result;
    }

    @Override
    public void updateUser(UserUpdateReqDTO requestParam) {
        if (!Objects.equals(UserContext.getUserAccount(), requestParam.getUserAccount())) {
            throw new ClientException("当前登录用户修改请求异常");
        }
        LambdaUpdateWrapper<UserDO> updateWrapper = Wrappers.lambdaUpdate(UserDO.class)
                .eq(UserDO::getUserAccount, requestParam.getUserAccount());
        baseMapper.update(BeanUtil.toBean(requestParam, UserDO.class), updateWrapper);

    }

    @Override
    public void logout(HttpServletRequest request) {
        String token = request.getHeader("token");
        if (token == null) {
            throw new ClientException("该用户未登陆");
        }
        try {
            stringRedisTemplate.opsForHash().delete(USER_LOGIN_KEY_USER_TO_TOKEN + UserContext.getUserAccount(), token);
            stringRedisTemplate.delete(USER_LOGIN_KEY_TOKEN_TO_USER + token);
        } catch (Exception ex) {
            throw new ClientException("用户登出错误,稍后再试");
        }
    }


    @Override
    public String getEncryptPassword(String userPassword) {
        // 可导入证书
        final String SALT="xixi";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

    /**
     * 寻找时间存在时间最长的token
     * @param tokens 输入token集合
     * @param tokenToUserKeyPrefix tokenToUser 的前缀
     * @return token
     */
    private String findTokenWithMinimalTTL(Set<Object> tokens, String tokenToUserKeyPrefix) {
        String minTTLToken = null;
        long minTTL = Long.MAX_VALUE;

        for (Object tokenObj : tokens) {
            String token = (String) tokenObj;
            Long ttl = stringRedisTemplate.getExpire(tokenToUserKeyPrefix + token, TimeUnit.SECONDS);
            if (ttl != null && ttl >= 0 && ttl < minTTL) {
                minTTL = ttl;
                minTTLToken = token;
            }
        }

        return minTTLToken;
    }


}




