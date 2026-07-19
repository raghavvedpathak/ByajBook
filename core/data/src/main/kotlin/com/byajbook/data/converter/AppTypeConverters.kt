package com.byajbook.data.converter

import androidx.room.TypeConverter
import com.byajbook.domain.model.RecordStatus
import com.byajbook.domain.model.RecordType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * [FIX-TIMESTAMP-STEP2-1] & H-2 FIX
 * Robust converters with nullable signatures and runCatching guards.
 */
class AppTypeConverters {
    private val dateDeformatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val dateTimeDeformatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { 
        runCatching { LocalDate.parse(it) }.getOrNull() 
    }

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? = value?.toString()

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? = value?.let { 
        runCatching { LocalDateTime.parse(it) }.getOrNull() 
    }

    @TypeConverter
    fun recordTypeToString(value: RecordType?): String? = value?.name

    @TypeConverter
    fun stringToRecordType(value: String?): RecordType? = value?.let { 
        runCatching { RecordType.valueOf(it) }.getOrNull() 
    }

    @TypeConverter
    fun recordStatusToString(value: RecordStatus?): String? = value?.name

    @TypeConverter
    fun stringToRecordStatus(value: String?): RecordStatus? = value?.let { 
        runCatching { RecordStatus.valueOf(it) }.getOrNull() 
    }
}
