package com.retailer.rewardpoints.controller;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.retailer.rewardpoints.dto.RewardPointsDto;
import com.retailer.rewardpoints.service.RewardPointService;

class RewardPointsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RewardPointService rewardService;

    @InjectMocks
    private RewardPointsController rewardController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(rewardController).build();
    }
    
    @Test
    void testGetRewardPoints_WithValidCustomerIdAndDates() throws Exception {
        Long customerId = 1L;
        LocalDate startDate = LocalDate.of(2025, 2, 1);
        LocalDate endDate = LocalDate.of(2025, 4, 30);

        RewardPointsDto rewardPointsDto = new RewardPointsDto(Map.of("FEBRUARY", 50, "MARCH", 30), 80);

        Mockito.when(rewardService.getRewardPoints(eq(customerId), eq(startDate), eq(endDate)))
                .thenReturn(rewardPointsDto);

        mockMvc.perform(get("/rewards/points/{customerId}", customerId)
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.rewardPointsDto.totalPoints", is(80)))
                .andExpect(jsonPath("$.rewardPointsDto.monthlyPoints.FEBRUARY", is(50)))
                .andExpect(jsonPath("$.rewardPointsDto.monthlyPoints.MARCH", is(30)));
    }
    @Test
    void testGetRewardPoints_WithValidCustomerIdAndNoDates() throws Exception {
        Long customerId = 2L;

        RewardPointsDto rewardPointsDto = new RewardPointsDto(Map.of("MARCH", 40, "APRIL", 60), 100);

        Mockito.when(rewardService.getRewardPoints(eq(customerId), isNull(), isNull()))
                .thenReturn(rewardPointsDto);

        mockMvc.perform(get("/rewards/points/{customerId}", customerId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.rewardPointsDto.totalPoints", is(100)))
                .andExpect(jsonPath("$.rewardPointsDto.monthlyPoints.MARCH", is(40)))
                .andExpect(jsonPath("$.rewardPointsDto.monthlyPoints.APRIL", is(60)));
    }

    @Test
    void testGetRewardPoints() throws Exception {
        Long customerId = 1L;
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 3, 31);

        RewardPointsDto rewardPointsDto = new RewardPointsDto();
        rewardPointsDto.setMonthlyPoints(Map.of("JANUARY", 90, "FEBRUARY", 30));
        rewardPointsDto.setTotalPoints(120);

        when(rewardService.getRewardPoints(customerId, startDate, endDate)).thenReturn(rewardPointsDto);

        mockMvc.perform(get("/rewards/points/{customerId}", customerId)
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyPoints.JANUARY").value(90))
                .andExpect(jsonPath("$.monthlyPoints.FEBRUARY").value(30))
                .andExpect(jsonPath("$.totalPoints").value(120));
    }

    @Test
    void testGetRewardPoints_InvalidDateRange() throws Exception {
        Long customerId = 1L;
        LocalDate startDate = LocalDate.of(2025, 4, 1);
        LocalDate endDate = LocalDate.of(2025, 1, 1);

        when(rewardService.getRewardPoints(customerId, startDate, endDate))
                .thenThrow(new IllegalArgumentException("start date must be earlier than the end date"));

        mockMvc.perform(get("/rewards/points/{customerId}", customerId)
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("start date must be earlier than the end date"));
    }
}
