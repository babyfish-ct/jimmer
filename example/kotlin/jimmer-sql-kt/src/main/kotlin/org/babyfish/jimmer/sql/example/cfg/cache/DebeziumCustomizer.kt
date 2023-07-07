package org.babyfish.jimmer.sql.example.cfg.cache

import org.apache.kafka.connect.data.Decimal
import org.babyfish.jimmer.sql.example.model.Book
import org.babyfish.jimmer.sql.kt.cfg.KCustomizer
import org.babyfish.jimmer.sql.kt.cfg.KSqlClientDsl
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

// -----------------------------
// If you are a beginner, please ignore this class,
// for non-cache mode, this class will never be used.
// -----------------------------
@ConditionalOnProperty(
    name = ["spring.profiles.active"],
    havingValue = "debezium"
)
@Component
class DebeziumCustomizer : KCustomizer {

    override fun customize(dsl: KSqlClientDsl) {

        dsl.setBinLogPropReader(
            LocalDateTime::class
        ) { _, jsonNode ->
            Instant.ofEpochMilli(
                jsonNode.asLong() / 1000
            ).atZone(ZoneId.systemDefault()).toLocalDateTime()
        }

        dsl.setBinLogPropReader(
            Book::price
        ) { _, jsonNode ->
            Decimal.toLogical(
                BOOK_PRICE_SCHEMA,
                Base64.getDecoder().decode(jsonNode.asText())
            )
        }
    }

    companion object {
        private val BOOK_PRICE_SCHEMA =
            // `BOOK.PRICE` of postgres is `NUMERIC(10, 2)`
            Decimal.schema(2)
    }
}