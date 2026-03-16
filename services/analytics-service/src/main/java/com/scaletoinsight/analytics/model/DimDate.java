package com.scaletoinsight.analytics.model;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "dim_date")
public class DimDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "date_key")
    private Integer dateKey;

    @Column(name = "full_date", nullable = false, unique = true)
    private LocalDate fullDate;

    @Column(name = "day_of_week", nullable = false)
    private Short dayOfWeek;

    @Column(name = "day_name", nullable = false)
    private String dayName;

    @Column(name = "month_number", nullable = false)
    private Short monthNumber;

    @Column(name = "month_name", nullable = false)
    private String monthName;

    @Column(name = "quarter", nullable = false)
    private Short quarter;

    @Column(name = "year", nullable = false)
    private Short year;

    @Column(name = "is_weekend", nullable = false)
    private boolean weekend;

    @Column(name = "is_holiday", nullable = false)
    private boolean holiday;

    public DimDate() {}

    // Getters & Setters
    public Integer getDateKey() { return dateKey; }
    public void setDateKey(Integer dateKey) { this.dateKey = dateKey; }

    public LocalDate getFullDate() { return fullDate; }
    public void setFullDate(LocalDate fullDate) { this.fullDate = fullDate; }

    public Short getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(Short dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public String getDayName() { return dayName; }
    public void setDayName(String dayName) { this.dayName = dayName; }

    public Short getMonthNumber() { return monthNumber; }
    public void setMonthNumber(Short monthNumber) { this.monthNumber = monthNumber; }

    public String getMonthName() { return monthName; }
    public void setMonthName(String monthName) { this.monthName = monthName; }

    public Short getQuarter() { return quarter; }
    public void setQuarter(Short quarter) { this.quarter = quarter; }

    public Short getYear() { return year; }
    public void setYear(Short year) { this.year = year; }

    public boolean isWeekend() { return weekend; }
    public void setWeekend(boolean weekend) { this.weekend = weekend; }

    public boolean isHoliday() { return holiday; }
    public void setHoliday(boolean holiday) { this.holiday = holiday; }
}
