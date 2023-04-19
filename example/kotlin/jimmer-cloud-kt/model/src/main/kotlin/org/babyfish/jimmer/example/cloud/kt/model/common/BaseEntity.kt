package org.babyfish.jimmer.example.cloud.kt.model.common

import org.babyfish.jimmer.sql.MappedSuperclass
import java.time.LocalTime

/*
 * Note: `acrossMicroServices = true`
 *
 * The super class across microservices can be inherited
 * by entity types belong any microservice, but it cannot
 * have association properties.
 *
 * Otherwise, the super class can have association properties
 * but super class and entity type must belong to one microservice
 */
@MappedSuperclass(acrossMicroServices = true)
interface BaseEntity {

    val createdTime: LocalTime

    val modifiedTime: LocalTime
}