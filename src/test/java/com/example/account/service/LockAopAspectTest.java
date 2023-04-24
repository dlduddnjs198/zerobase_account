package com.example.account.service;

import com.example.account.dto.UseBalance;
import com.example.account.exception.AccountException;
import com.example.account.type.ErrorCode;
import net.bytebuddy.asm.Advice;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LockAopAspectTest {
    @Mock
    private LockService lockService;
    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;
    @InjectMocks
    private LockAopAspect lockAopAspect;
    @Test
    void lockAndUnlock_evenIfThrow() throws Throwable {
        //given(어떤 데이터가 있을때)
        ArgumentCaptor<String> lockArgumentCaptor=
                ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> unLockArgumentCaptor=
                ArgumentCaptor.forClass(String.class);
        UseBalance.Request request =
                new UseBalance.Request(123L, "54321", 1000L);
        given(proceedingJoinPoint.proceed())
                .willThrow(new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));
        //when(어떤 동작을 하게되면)
        assertThrows(AccountException.class, () ->
                lockAopAspect.aroundMethod(proceedingJoinPoint, request));

        //then(어떤 결과가 나와야한다)
        verify(lockService, times(1)).lock(lockArgumentCaptor.capture());
        verify(lockService, times(1)).unlock(unLockArgumentCaptor.capture());
        assertEquals("54321", lockArgumentCaptor.getValue());
        assertEquals("54321", unLockArgumentCaptor.getValue());
    }

}