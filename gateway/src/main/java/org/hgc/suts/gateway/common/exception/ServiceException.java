package org.hgc.suts.gateway.common.exception;


import org.hgc.suts.gateway.common.errorcode.BaseErrorCode;
import org.hgc.suts.gateway.common.errorcode.IErrorCode;

import java.util.Optional;

/**
 * 服务端运行异常｜请求运行过程中出现的不符合业务预期的异常
 */
public class ServiceException extends AbstractException {

    public ServiceException(String message) {
        this(message, null, BaseErrorCode.SERVICE_ERROR);
    }

    public ServiceException(IErrorCode errorCode) {
        this(null, errorCode);
    }

    public ServiceException(String message, IErrorCode errorCode) {
        this(message, null, errorCode);
    }

    public ServiceException(String message, Throwable throwable, IErrorCode errorCode) {
        super(Optional.ofNullable(message).orElse(errorCode.message()), throwable, errorCode);
    }

    @Override
    public String toString() {
        return "ServiceException{" +
                "code='" + errorCode + "'," +
                "message='" + errorMessage + "'" +
                '}';
    }
}
