package org.babyfish.jimmer.sql.kt.model.pg

import org.babyfish.jimmer.sql.runtime.ScalarProvider
import org.postgresql.util.PGobject
import java.net.InetAddress

class InetAddressProvider : ScalarProvider<InetAddress, PGobject> {

    override fun toScalar(sqlValue: PGobject): InetAddress =
        InetAddress.getByName(sqlValue.value)

    override fun toSql(scalarValue: InetAddress): PGobject =
        PGobject().apply {
            type = "inet"
            value = scalarValue.hostAddress
        }
}