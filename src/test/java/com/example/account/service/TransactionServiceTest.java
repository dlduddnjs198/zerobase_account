package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.domain.Transaction;
import com.example.account.dto.TransactionDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.repository.TransactionRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.example.account.type.AccountStatus.IN_USE;
import static com.example.account.type.TransactionResultType.F;
import static com.example.account.type.TransactionResultType.S;
import static com.example.account.type.TransactionType.CANCEL;
import static com.example.account.type.TransactionType.USE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    public static final long USE_AMOUNT = 200L;
    public static final long CANCEL_AMOUNT = 200L;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void successUseBalance() {
        //given
        AccountUser user = AccountUser.builder()
                .name("Pobi").build();
        user.setId(12L);

        //Optional을 toString()으로 바꿀 수 없다는게 도대체 무슨 소리일까...
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(USE)
                        .transactionResultType(S)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .amount(1000L)
                        .balanceSnapshot(9000L)
                        .build());

        //argumentcaptor를 통해 save에서 제대로 값이 저장되는지를 확인할 수 있다.
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);


        //when(어떤 동작을 하게되면)
        TransactionDto transactionDto = transactionService.useBalance(1L, "1000000000", USE_AMOUNT);

        //then(어떤 결과가 나와야한다)
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(USE_AMOUNT, captor.getValue().getAmount());
        assertEquals(9800L, captor.getValue().getBalanceSnapshot());
        assertEquals(S, transactionDto.getTransactionResultType());
        assertEquals(USE, transactionDto.getTransactionType());
        assertEquals(9000L, transactionDto.getBalanceSnapshot());
        assertEquals(1000L, transactionDto.getAmount());
    }

    @Test
    @DisplayName("해당 유저 없음 - 잔액 사용 실패")
    void useBalance_UserNotFound() {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1111111111", 1000L));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("해당 계좌 없음 - 잔액 사용 실패")
    void deleteAccount_AccountNotFound() {
        //given
        AccountUser user = AccountUser.builder()
                .name("Pobi").build();
        user.setId(12L);

        //Optional을 toString()으로 바꿀 수 없다는게 도대체 무슨 소리일까...
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1111111111", 1000L));

        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 소유주 다름 - 잔액 사용 실패")
    void deleteAccountFailed_userUnMatch() {
        //given
        AccountUser pobi = AccountUser.builder()
                .name("Pobi").build();
        pobi.setId(12L);
        AccountUser harry = AccountUser.builder()
                .name("Harry").build();
        harry.setId(13L);

        //Optional을 toString()으로 바꿀 수 없다는게 도대체 무슨 소리일까...
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(harry)
                        .balance(0L)
                        .accountNumber("1000000012").build()));
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1111111111", 1000L));

        //then
        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("해지 계좌는 사용할 수 없다.")
    void deleteAccountFailed_alreadyUnregistered() {
        //given
        AccountUser pobi = AccountUser.builder()
                .name("Pobi").build();
        pobi.setId(12L);
        //Optional을 toString()으로 바꿀 수 없다는게 도대체 무슨 소리일까...
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(pobi)
                        .accountStatus(AccountStatus.UNREGISTERED)
                        .balance(0L)
                        .accountNumber("1000000012").build()));
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1111111111", 1000L));

        //then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());
    }

    @Test
    @DisplayName("거래 금액이 잔액보다 큰 경우")
    void exceedAmount_UseBalance() {
        //given
        AccountUser user = AccountUser.builder()
                .name("Pobi").build();
        user.setId(12L);

        //Optional을 toString()으로 바꿀 수 없다는게 도대체 무슨 소리일까...
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(100L)
                .accountNumber("1000000012").build();
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //argumentcaptor를 통해 save에서 제대로 값이 저장되는지를 확인할 수 있다.


        //then(어떤 결과가 나와야한다)
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1111111111", 1000L));
        assertEquals(ErrorCode.AMOUNT_EXCEED_BALANCE, exception.getErrorCode());
        verify(transactionRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("실패 트랜잭션 저장 성공")
    void saveFailedUseTransaction() {
        //given
        AccountUser user = AccountUser.builder()
                .name("Pobi").build();
        user.setId(12L);

        //Optional을 toString()으로 바꿀 수 없다는게 도대체 무슨 소리일까...
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(USE)
                        .transactionResultType(S)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .amount(1000L)
                        .balanceSnapshot(9000L)
                        .build());

        //argumentcaptor를 통해 save에서 제대로 값이 저장되는지를 확인할 수 있다.
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);


        //when(어떤 동작을 하게되면)
        //void 타입이라 변수 필요없다. saveFailed 자체를 확인하는거라.
        transactionService.saveFailedUseTransaction("1000000000", USE_AMOUNT);

        //then(어떤 결과가 나와야한다)
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(USE_AMOUNT, captor.getValue().getAmount());
        assertEquals(10000L, captor.getValue().getBalanceSnapshot());
        assertEquals(F, captor.getValue().getTransactionResultType());
    }

    @Test
    void successCancelBalance() {
        //given
        AccountUser user = AccountUser.builder()
                .name("Pobi").build();
        user.setId(12L);
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(9000L)
                .build();
        //Optional을 toString()으로 바꿀 수 없다는게 도대체 무슨 소리일까...
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(CANCEL)
                        .transactionResultType(S)
                        .transactionId("transactionIdForCancel")
                        .transactedAt(LocalDateTime.now())
                        .amount(CANCEL_AMOUNT)
                        .balanceSnapshot(10000L)
                        .build());

        //argumentcaptor를 통해 save에서 제대로 값이 저장되는지를 확인할 수 있다.
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);


        //when(어떤 동작을 하게되면)
        TransactionDto transactionDto = transactionService.cancelBalance("transactionId",
                "1000000000", CANCEL_AMOUNT);

        //then(어떤 결과가 나와야한다)
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(CANCEL_AMOUNT, captor.getValue().getAmount());
        assertEquals(10000L + CANCEL_AMOUNT, captor.getValue().getBalanceSnapshot());
        assertEquals(S, transactionDto.getTransactionResultType());
        assertEquals(CANCEL, transactionDto.getTransactionType());
        assertEquals(10000L, transactionDto.getBalanceSnapshot());
        assertEquals(CANCEL_AMOUNT, transactionDto.getAmount());
    }

    @Test
    @DisplayName("해당 계좌 없음 - 잔액 사용 취소 실패")
    void cancelTransaction_AccountNotFound() {
        //given

        //Optional을 toString()으로 바꿀 수 없다는게 도대체 무슨 소리일까...
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(Transaction.builder().build()));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1111111111", 1000L));

        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("원 사용 거래 없음 - 잔액 사용 취소 실패")
    void cancelTransaction_TransactionNotFound() {
        //given

        //Optional을 toString()으로 바꿀 수 없다는게 도대체 무슨 소리일까...
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1111111111", 1000L));

        //then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("거래와 계좌가 매칭 실패 - 잔액 사용 취소 실패")
    void cancelTransaction_TransactionAccountUnMatch() {
        //given
        AccountUser user = AccountUser.builder()
                .name("Pobi").build();
        user.setId(12L);
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        account.setId(1L);
        Account accountNotUse = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000013").build();
        accountNotUse.setId(2L);
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(9000L)
                .build();
        //Optional을 toString()으로 바꿀 수 없다는게 도대체 무슨 소리일까...
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(accountNotUse));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        "transactionId",
                        "1111111111",
                        CANCEL_AMOUNT
                )
        );

        //then
        assertEquals(ErrorCode.TRANSACTION_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("거래 금액과 취소 금액이 다름 - 잔액 사용 취소 실패")
    void cancelTransaction_CancelMustFully() {
        //given
        AccountUser user = AccountUser.builder()
                .name("Pobi").build();
        user.setId(12L);
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        account.setId(1L);
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(CANCEL_AMOUNT + 1000L)
                .balanceSnapshot(9000L)
                .build();
        //Optional을 toString()으로 바꿀 수 없다는게 도대체 무슨 소리일까...
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        "transactionId",
                        "1111111111",
                        CANCEL_AMOUNT
                )
        );

        //then
        assertEquals(ErrorCode.CANCEL_MUST_FULLY, exception.getErrorCode());
    }

    @Test
    @DisplayName("취소는 1년까지만 가능 - 잔액 사용 취소 실패")
    void cancelTransaction_TooOldOrder() {
        //given
        AccountUser user = AccountUser.builder()
                .name("Pobi").build();
        user.setId(12L);
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        account.setId(1L);
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now().minusYears(1).minusDays(1))
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(9000L)
                .build();
        //Optional을 toString()으로 바꿀 수 없다는게 도대체 무슨 소리일까...
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        "transactionId",
                        "1111111111",
                        CANCEL_AMOUNT
                )
        );

        //then
        assertEquals(ErrorCode.TOO_OLD_ORDER_TO_CANCEL, exception.getErrorCode());
    }

    @Test
    void successQueryTransaction() {
        //given(어떤 데이터가 있을때)
        AccountUser user = AccountUser.builder()
                .name("Pobi").build();
        user.setId(12L);
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        account.setId(1L);
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now().minusYears(1).minusDays(1))
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(9000L)
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        //when(어떤 동작을 하게되면)
        TransactionDto transactionDto = transactionService.queryTransaction("trxId");
        //then(어떤 결과가 나와야한다)
        assertEquals(USE, transactionDto.getTransactionType());
        assertEquals(S, transactionDto.getTransactionResultType());
        assertEquals(CANCEL_AMOUNT, transactionDto.getAmount());
        assertEquals("transactionId", transactionDto.getTransactionId());
    }

    @Test
    @DisplayName("원거래 없음 - 잔액 조회 실패")
    void queryTransaction_TransactionNotFound() {
        //given

        //Optional을 toString()으로 바꿀 수 없다는게 도대체 무슨 소리일까...
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.queryTransaction("transactionId"));

        //then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }
}