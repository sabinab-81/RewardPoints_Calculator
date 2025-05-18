package com.retailer.rewardpoints.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.retailer.rewardpoints.dao.TransactionRepository;
import com.retailer.rewardpoints.dto.RewardPointsDto;
import com.retailer.rewardpoints.entity.Transaction;

class RewardPointServiceImplTest {

	@Mock
	private TransactionRepository transactionRepository;

	@InjectMocks
	private RewardPointServiceImpl rewardPointServiceImpl;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}
	
	   @Test
	    void testCalculatePoints() {
	        assertThat(rewardPointServiceImpl.calculatePoints(120.0)).isEqualTo(90);
	        assertThat(rewardPointServiceImpl.calculatePoints(75.0)).isEqualTo(25);
	        assertThat(rewardPointServiceImpl.calculatePoints(45.0)).isEqualTo(0);
	    }
    
    @Test
    public void testCalculatePoints_AmountLessThan50() {
        int points = rewardPointServiceImpl.calculatePoints(30.0);
        assertEquals(0, points);
    }

    @Test
    public void testCalculatePoints_AmountBetween50And100() {
        int points = rewardPointServiceImpl.calculatePoints(75.0);
        assertEquals(25, points);
    }

    @Test
    public void testCalculatePoints_AmountGreaterThan100() {
        int points = rewardPointServiceImpl.calculatePoints(150.0);
        assertEquals(150, points);
    }

    @Test
    public void testCalculateMonthlyPoints() {
    	rewardPointServiceImpl.transactionRepository = transactionRepository;

        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 3, 31);
        Long customerId = 1L;

        Transaction transaction1 = new Transaction(1L, customerId, 120.0, LocalDate.of(2025, 1, 15));
        Transaction transaction2 = new Transaction(2L, customerId, 80.0, LocalDate.of(2025, 2, 20));
        Transaction transaction3 = new Transaction(3L, customerId, 50.0, LocalDate.of(2025, 3, 10));

        List<Transaction> transactions = Arrays.asList(transaction1, transaction2, transaction3);
        Mockito.when(transactionRepository.findByCustomerIdAndDateBetween(customerId, startDate, endDate)).thenReturn(transactions);

        RewardPointsDto result = rewardPointServiceImpl.calculateMonthlyPoints(customerId, startDate, endDate);

        assertEquals(3, result.getMonthlyPoints().size());
        assertEquals(90, result.getMonthlyPoints().get("JANUARY"));
        assertEquals(30, result.getMonthlyPoints().get("FEBRUARY"));
        assertEquals(0, result.getMonthlyPoints().get("MARCH"));
        assertEquals(120, result.getTotalPoints());
    }

    @Test
    public void testValidateAndAdjustDates_BothDatesNull() {
        Map<String, Object> result = rewardPointServiceImpl.validateAndAdjustDates(null, null);
        LocalDate expectedEndDate = LocalDate.now();
        LocalDate expectedStartDate = expectedEndDate.minusMonths(3);
        assertEquals(expectedStartDate, result.get("startDate"));
        assertEquals(expectedEndDate, result.get("endDate"));
    }
    @Test
    void testValidateAndAdjustDates_StartDateOnly() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        Map<String, Object> result = rewardPointServiceImpl.validateAndAdjustDates(start, null);

        assertThat(result).containsKeys("startDate", "endDate");
        assertThat(result.get("startDate")).isEqualTo(start);
        assertThat(result.get("endDate")).isEqualTo(start.plusMonths(3));
    }
    @Test
    void testValidateAndAdjustDates_EndDateOnly() {
        LocalDate end = LocalDate.of(2025, 4, 30);
        Map<String, Object> result = rewardPointServiceImpl.validateAndAdjustDates(null, end);

        assertThat(result).containsKeys("startDate", "endDate");
        assertThat(result.get("startDate")).isEqualTo(end.minusMonths(3));
        assertThat(result.get("endDate")).isEqualTo(end);
    }

    @Test
    public void testValidateAndAdjustDates_StartDateProvided_EndDateNull() {
        LocalDate startDate = LocalDate.of(2025, 4, 29);
        Map<String, Object> result = rewardPointServiceImpl.validateAndAdjustDates(startDate, null);
        LocalDate expectedEndDate = startDate.plusMonths(3);
        assertEquals(startDate, result.get("startDate"));
        assertEquals(expectedEndDate, result.get("endDate"));
    }

    @Test
    public void testValidateAndAdjustDates_StartDateNull_EndDateProvided() {
        LocalDate endDate = LocalDate.of(2025, 4, 29);
        Map<String, Object> result = rewardPointServiceImpl.validateAndAdjustDates(null, endDate);
        LocalDate expectedStartDate = endDate.minusMonths(3);
        assertEquals(expectedStartDate, result.get("startDate"));
        assertEquals(endDate, result.get("endDate"));
    }

    @Test
    public void testValidateAndAdjustDates_StartDateAfterEndDate() {
        LocalDate startDate = LocalDate.of(2025, 5, 1);
        LocalDate endDate = LocalDate.of(2025, 4, 29);
        Map<String, Object> result = rewardPointServiceImpl.validateAndAdjustDates(startDate, endDate);
        assertEquals("start date must be earlier than the end date.", result.get("error"));
    }

    @Test
    public void testValidateAndAdjustDates_DateRangeExceedsThreeMonths() {
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 4, 29);
        Map<String, Object> result = rewardPointServiceImpl.validateAndAdjustDates(startDate, endDate);
        assertEquals("Date range should not exceed three months.", result.get("error"));
    }
  

    @Test
    public void testGetRewardPoints_InvalidDateRange() {
        LocalDate startDate = LocalDate.of(2025, 5, 1);
        LocalDate endDate = LocalDate.of(2025, 4, 29);
        Long customerId = 1L;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
        	rewardPointServiceImpl.getRewardPoints(customerId, startDate, endDate);
        });

        assertEquals("start date must be earlier than the end date.", exception.getMessage());
    }

    }