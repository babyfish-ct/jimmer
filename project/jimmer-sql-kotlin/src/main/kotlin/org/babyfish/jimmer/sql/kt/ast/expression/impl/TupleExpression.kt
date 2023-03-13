package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.Expression
import org.babyfish.jimmer.sql.ast.Selection
import org.babyfish.jimmer.sql.ast.impl.Ast
import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.ast.impl.TupleExpressionImplementor
import org.babyfish.jimmer.sql.ast.tuple.*
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.runtime.SqlBuilder

internal class Tuple2Expression<T1, T2>(
    private val selection1: Selection<T1>,
    private val selection2: Selection<T2>
): AbstractKExpression<Tuple2<T1, T2>>(),
    KNonNullExpression<Tuple2<T1, T2>>,
    TupleExpressionImplementor<Tuple2<T1, T2>> {
    init {
        if (selection1 !is KExpression<*>) {
            throw IllegalArgumentException("selection1 is not KExpression")
        }
        if (selection2 !is KExpression<*>) {
            throw IllegalArgumentException("selection2 is not KExpression")
        }
    }

    override fun precedence(): Int = 0

    @Suppress("UNCHECKED_CAST")
    override fun getType(): Class<Tuple2<T1, T2>> =
        Tuple2::class.java as Class<Tuple2<T1, T2>>

    override fun accept(visitor: AstVisitor) {
        (selection1 as Ast).accept(visitor)
        (selection2 as Ast).accept(visitor)
    }

    override fun renderTo(builder: SqlBuilder) {
        usingLowestPrecedence {
            builder.sql("(")
            (selection1 as Ast).renderTo(builder)
            builder.sql(", ")
            (selection2 as Ast).renderTo(builder)
            builder.sql(")")
        }
    }

    override fun size(): Int = 2

    override operator fun get(index: Int): Selection<*> =
        when (index) {
            0 -> selection1
            1 -> selection2
            else -> throw IllegalArgumentException("index must between 0 and ${size() - 1}")
        }
}

internal class Tuple3Expression<T1, T2, T3>(
    private val selection1: Selection<T1>,
    private val selection2: Selection<T2>,
    private val selection3: Selection<T3>
): AbstractKExpression<Tuple3<T1, T2, T3>>(),
    KNonNullExpression<Tuple3<T1, T2, T3>>,
    TupleExpressionImplementor<Tuple3<T1, T2, T3>> {
    init {
        if (selection1 !is KExpression<*>) {
            throw IllegalArgumentException("selection1 is not KExpression")
        }
        if (selection2 !is KExpression<*>) {
            throw IllegalArgumentException("selection2 is not KExpression")
        }
        if (selection3 !is KExpression<*>) {
            throw IllegalArgumentException("selection3 is not KExpression")
        }
    }

    override fun precedence(): Int = 0

    @Suppress("UNCHECKED_CAST")
    override fun getType(): Class<Tuple3<T1, T2, T3>> =
        Tuple3::class.java as Class<Tuple3<T1, T2, T3>>

    override fun accept(visitor: AstVisitor) {
        (selection1 as Ast).accept(visitor)
        (selection2 as Ast).accept(visitor)
        (selection3 as Ast).accept(visitor)
    }

    override fun renderTo(builder: SqlBuilder) {
        usingLowestPrecedence {
            builder.sql("(")
            (selection1 as Ast).renderTo(builder)
            builder.sql(", ")
            (selection2 as Ast).renderTo(builder)
            builder.sql(", ")
            (selection3 as Ast).renderTo(builder)
            builder.sql(")")
        }
    }

    override fun size(): Int = 3

    override operator fun get(index: Int): Selection<*> =
        when (index) {
            0 -> selection1
            1 -> selection2
            2 -> selection3
            else -> throw IllegalArgumentException("index must between 0 and ${size() - 1}")
        }
}

internal class Tuple4Expression<T1, T2, T3, T4>(
    private val selection1: Selection<T1>,
    private val selection2: Selection<T2>,
    private val selection3: Selection<T3>,
    private val selection4: Selection<T4>
): AbstractKExpression<Tuple4<T1, T2, T3, T4>>(),
    KNonNullExpression<Tuple4<T1, T2, T3, T4>>,
    TupleExpressionImplementor<Tuple4<T1, T2, T3, T4>> {
    init {
        if (selection1 !is KExpression<*>) {
            throw IllegalArgumentException("selection1 is not KExpression")
        }
        if (selection2 !is KExpression<*>) {
            throw IllegalArgumentException("selection2 is not KExpression")
        }
        if (selection3 !is KExpression<*>) {
            throw IllegalArgumentException("selection3 is not KExpression")
        }
        if (selection4 !is KExpression<*>) {
            throw IllegalArgumentException("selection4 is not KExpression")
        }
    }

    override fun precedence(): Int = 0

    @Suppress("UNCHECKED_CAST")
    override fun getType(): Class<Tuple4<T1, T2, T3, T4>> =
        Tuple4::class.java as Class<Tuple4<T1, T2, T3, T4>>

    override fun accept(visitor: AstVisitor) {
        (selection1 as Ast).accept(visitor)
        (selection2 as Ast).accept(visitor)
        (selection3 as Ast).accept(visitor)
        (selection4 as Ast).accept(visitor)
    }

    override fun renderTo(builder: SqlBuilder) {
        usingLowestPrecedence {
            builder.sql("(")
            (selection1 as Ast).renderTo(builder)
            builder.sql(", ")
            (selection2 as Ast).renderTo(builder)
            builder.sql(", ")
            (selection3 as Ast).renderTo(builder)
            builder.sql(", ")
            (selection4 as Ast).renderTo(builder)
            builder.sql(")")
        }
    }

    override fun size(): Int = 4

    override operator fun get(index: Int): Selection<*> =
        when (index) {
            0 -> selection1
            1 -> selection2
            2 -> selection3
            3 -> selection4
            else -> throw IllegalArgumentException("index must between 0 and ${size() - 1}")
        }
}

internal class Tuple5Expression<T1, T2, T3, T4, T5>(
    private val selection1: Selection<T1>,
    private val selection2: Selection<T2>,
    private val selection3: Selection<T3>,
    private val selection4: Selection<T4>,
    private val selection5: Selection<T5>
): AbstractKExpression<Tuple5<T1, T2, T3, T4, T5>>(),
    KNonNullExpression<Tuple5<T1, T2, T3, T4, T5>>,
    TupleExpressionImplementor<Tuple5<T1, T2, T3, T4, T5>> {
    init {
        if (selection1 !is KExpression<*>) {
            throw IllegalArgumentException("selection1 is not KExpression")
        }
        if (selection2 !is KExpression<*>) {
            throw IllegalArgumentException("selection2 is not KExpression")
        }
        if (selection3 !is KExpression<*>) {
            throw IllegalArgumentException("selection3 is not KExpression")
        }
        if (selection4 !is KExpression<*>) {
            throw IllegalArgumentException("selection4 is not KExpression")
        }
        if (selection5 !is KExpression<*>) {
            throw IllegalArgumentException("selection5 is not KExpression")
        }
    }

    override fun precedence(): Int = 0

    @Suppress("UNCHECKED_CAST")
    override fun getType(): Class<Tuple5<T1, T2, T3, T4, T5>> =
        Tuple5::class.java as Class<Tuple5<T1, T2, T3, T4, T5>>

    override fun accept(visitor: AstVisitor) {
        (selection1 as Ast).accept(visitor)
        (selection2 as Ast).accept(visitor)
        (selection3 as Ast).accept(visitor)
        (selection4 as Ast).accept(visitor)
        (selection5 as Ast).accept(visitor)
    }

    override fun renderTo(builder: SqlBuilder) {
        usingLowestPrecedence {
            builder.sql("(")
            (selection1 as Ast).renderTo(builder)
            builder.sql(", ")
            (selection2 as Ast).renderTo(builder)
            builder.sql(", ")
            (selection3 as Ast).renderTo(builder)
            builder.sql(", ")
            (selection4 as Ast).renderTo(builder)
            builder.sql(", ")
            (selection5 as Ast).renderTo(builder)
            builder.sql(")")
        }
    }

    override fun size(): Int = 5

    override operator fun get(index: Int): Selection<*> =
        when (index) {
            0 -> selection1
            1 -> selection2
            2 -> selection3
            3 -> selection4
            4 -> selection5
            else -> throw IllegalArgumentException("index must between 0 and ${size() - 1}")
        }
}

internal class Tuple6Expression<T1, T2, T3, T4, T5, T6>(
    private val selection1: Selection<T1>,
    private val selection2: Selection<T2>,
    private val selection3: Selection<T3>,
    private val selection4: Selection<T4>,
    private val selection5: Selection<T5>,
    private val selection6: Selection<T6>
): AbstractKExpression<Tuple6<T1, T2, T3, T4, T5, T6>>(),
    KNonNullExpression<Tuple6<T1, T2, T3, T4, T5, T6>>,
    TupleExpressionImplementor<Tuple6<T1, T2, T3, T4, T5, T6>>{
    init {
        if (selection1 !is KExpression<*>) {
            throw IllegalArgumentException("selection1 is not KExpression")
        }
        if (selection2 !is KExpression<*>) {
            throw IllegalArgumentException("selection2 is not KExpression")
        }
        if (selection3 !is KExpression<*>) {
            throw IllegalArgumentException("selection3 is not KExpression")
        }
        if (selection4 !is KExpression<*>) {
            throw IllegalArgumentException("selection4 is not KExpression")
        }
        if (selection5 !is KExpression<*>) {
            throw IllegalArgumentException("selection5 is not KExpression")
        }
        if (selection6 !is KExpression<*>) {
            throw IllegalArgumentException("selection6 is not KExpression")
        }
    }

    override fun precedence(): Int = 0

    @Suppress("UNCHECKED_CAST")
    override fun getType(): Class<Tuple6<T1, T2, T3, T4, T5, T6>> =
        Tuple6::class.java as Class<Tuple6<T1, T2, T3, T4, T5, T6>>

    override fun accept(visitor: AstVisitor) {
        (selection1 as Ast).accept(visitor)
        (selection2 as Ast).accept(visitor)
        (selection3 as Ast).accept(visitor)
        (selection4 as Ast).accept(visitor)
        (selection5 as Ast).accept(visitor)
        (selection6 as Ast).accept(visitor)
    }

    override fun renderTo(builder: SqlBuilder) {
        usingLowestPrecedence {
            builder.sql("(")
            (selection1 as Ast).renderTo(builder)
            builder.sql(", ")
            (selection2 as Ast).renderTo(builder)
            builder.sql(", ")
            (selection3 as Ast).renderTo(builder)
            builder.sql(", ")
            (selection4 as Ast).renderTo(builder)
            builder.sql(", ")
            (selection5 as Ast).renderTo(builder)
            builder.sql(", ")
            (selection6 as Ast).renderTo(builder)
            builder.sql(")")
        }
    }

    override fun size(): Int = 6

    override operator fun get(index: Int): Selection<*> =
        when (index) {
            0 -> selection1
            1 -> selection2
            2 -> selection3
            3 -> selection4
            4 -> selection5
            5 -> selection6
            else -> throw IllegalArgumentException("index must between 0 and ${size() - 1}")
        }
}

internal class Tuple7Expression<T1, T2, T3, T4, T5, T6, T7>(
    private val selection1: Selection<T1>,
    private val selection2: Selection<T2>,
    private val selection3: Selection<T3>,
    private val selection4: Selection<T4>,
    private val selection5: Selection<T5>,
    private val selection6: Selection<T6>,
    private val selection7: Selection<T7>
): AbstractKExpression<Tuple7<T1, T2, T3, T4, T5, T6, T7>>(),
    KNonNullExpression<Tuple7<T1, T2, T3, T4, T5, T6, T7>>,
    TupleExpressionImplementor<Tuple7<T1, T2, T3, T4, T5, T6, T7>>{
    init {
        if (selection1 !is KExpression<*>) {
            throw IllegalArgumentException("selection1 is not KExpression")
        }
        if (selection2 !is KExpression<*>) {
            throw IllegalArgumentException("selection2 is not KExpression")
        }
        if (selection3 !is KExpression<*>) {
            throw IllegalArgumentException("selection3 is not KExpression")
        }
        if (selection4 !is KExpression<*>) {
            throw IllegalArgumentException("selection4 is not KExpression")
        }
        if (selection5 !is KExpression<*>) {
            throw IllegalArgumentException("selection5 is not KExpression")
        }
        if (selection6 !is KExpression<*>) {
            throw IllegalArgumentException("selection6 is not KExpression")
        }
        if (selection7 !is KExpression<*>) {
            throw IllegalArgumentException("selection7 is not KExpression")
        }
    }

    override fun precedence(): Int = 0

    @Suppress("UNCHECKED_CAST")
    override fun getType(): Class<Tuple7<T1, T2, T3, T4, T5, T6, T7>> =
        Tuple7::class.java as Class<Tuple7<T1, T2, T3, T4, T5, T6, T7>>

    override fun accept(visitor: AstVisitor) {
        (selection1 as Ast).accept(visitor)
        (selection2 as Ast).accept(visitor)
        (selection3 as Ast).accept(visitor)
        (selection4 as Ast).accept(visitor)
        (selection5 as Ast).accept(visitor)
        (selection6 as Ast).accept(visitor)
        (selection7 as Ast).accept(visitor)
    }

    override fun renderTo(builder: SqlBuilder) {
        usingLowestPrecedence {
            builder.sql("(")
            (selection1 as Ast).renderTo(builder)
            builder.sql(", ")
            (selection2 as Ast).renderTo(builder)
            builder.sql(", ")
            (selection3 as Ast).renderTo(builder)
            builder.sql(", ")
            (selection4 as Ast).renderTo(builder)
            builder.sql(", ")
            (selection5 as Ast).renderTo(builder)
            builder.sql(", ")
            (selection6 as Ast).renderTo(builder)
            builder.sql(", ")
            (selection7 as Ast).renderTo(builder)
            builder.sql(")")
        }
    }

    override fun size(): Int = 7

    override operator fun get(index: Int): Selection<*> =
        when (index) {
            0 -> selection1
            1 -> selection2
            2 -> selection3
            3 -> selection4
            4 -> selection5
            5 -> selection6
            6 -> selection7
            else -> throw IllegalArgumentException("index must between 0 and ${size() - 1}")
        }
}

internal class Tuple8Expression<T1, T2, T3, T4, T5, T6, T7, T8>(
    private val selection1: Selection<T1>,
    private val selection2: Selection<T2>,
    private val selection3: Selection<T3>,
    private val selection4: Selection<T4>,
    private val selection5: Selection<T5>,
    private val selection6: Selection<T6>,
    private val selection7: Selection<T7>,
    private val selection8: Selection<T8>
): AbstractKExpression<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>>(),
    KNonNullExpression<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>>,
    TupleExpressionImplementor<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>>{
    init {
        if (selection1 !is KExpression<*>) {
            throw IllegalArgumentException("selection1 is not KExpression")
        }
        if (selection2 !is KExpression<*>) {
            throw IllegalArgumentException("selection2 is not KExpression")
        }
        if (selection3 !is KExpression<*>) {
            throw IllegalArgumentException("selection3 is not KExpression")
        }
        if (selection4 !is KExpression<*>) {
            throw IllegalArgumentException("selection4 is not KExpression")
        }
        if (selection5 !is KExpression<*>) {
            throw IllegalArgumentException("selection5 is not KExpression")
        }
        if (selection6 !is KExpression<*>) {
            throw IllegalArgumentException("selection6 is not KExpression")
        }
        if (selection7 !is KExpression<*>) {
            throw IllegalArgumentException("selection7 is not KExpression")
        }
        if (selection8 !is KExpression<*>) {
            throw IllegalArgumentException("selection8 is not KExpression")
        }
    }

    override fun precedence(): Int = 0

    @Suppress("UNCHECKED_CAST")
    override fun getType(): Class<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> =
        Tuple8::class.java as Class<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>>

    override fun accept(visitor: AstVisitor) {
        (selection1 as Ast).accept(visitor)
        (selection2 as Ast).accept(visitor)
        (selection3 as Ast).accept(visitor)
        (selection4 as Ast).accept(visitor)
        (selection5 as Ast).accept(visitor)
        (selection6 as Ast).accept(visitor)
        (selection7 as Ast).accept(visitor)
        (selection8 as Ast).accept(visitor)
    }

    override fun renderTo(builder: SqlBuilder) {
        usingLowestPrecedence {
            builder.sql("(")
            (selection1 as Ast).renderTo(builder)
            builder.sql(", ")
            (selection2 as Ast).renderTo(builder)
            builder.sql(", ")
            (selection3 as Ast).renderTo(builder)
            builder.sql(", ")
            (selection4 as Ast).renderTo(builder)
            builder.sql(", ")
            (selection5 as Ast).renderTo(builder)
            builder.sql(", ")
            (selection6 as Ast).renderTo(builder)
            builder.sql(", ")
            (selection7 as Ast).renderTo(builder)
            builder.sql(", ")
            (selection8 as Ast).renderTo(builder)
            builder.sql(")")
        }
    }

    override fun size(): Int = 8

    override operator fun get(index: Int): Selection<*> =
        when (index) {
            0 -> selection1
            1 -> selection2
            2 -> selection3
            3 -> selection4
            4 -> selection5
            5 -> selection6
            6 -> selection7
            7 -> selection8
            else -> throw IllegalArgumentException("index must between 0 and ${size() - 1}")
        }
}

internal class Tuple9Expression<T1, T2, T3, T4, T5, T6, T7, T8, T9>(
    private val selection1: Selection<T1>,
    private val selection2: Selection<T2>,
    private val selection3: Selection<T3>,
    private val selection4: Selection<T4>,
    private val selection5: Selection<T5>,
    private val selection6: Selection<T6>,
    private val selection7: Selection<T7>,
    private val selection8: Selection<T8>,
    private val selection9: Selection<T9>
): AbstractKExpression<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>>(), 
    KNonNullExpression<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>>,
    TupleExpressionImplementor<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> {
    init {
        if (selection1 !is KExpression<*>) {
            throw IllegalArgumentException("selection1 is not KExpression")
        }
        if (selection2 !is KExpression<*>) {
            throw IllegalArgumentException("selection2 is not KExpression")
        }
        if (selection3 !is KExpression<*>) {
            throw IllegalArgumentException("selection3 is not KExpression")
        }
        if (selection4 !is KExpression<*>) {
            throw IllegalArgumentException("selection4 is not KExpression")
        }
        if (selection5 !is KExpression<*>) {
            throw IllegalArgumentException("selection5 is not KExpression")
        }
        if (selection6 !is KExpression<*>) {
            throw IllegalArgumentException("selection6 is not KExpression")
        }
        if (selection7 !is KExpression<*>) {
            throw IllegalArgumentException("selection7 is not KExpression")
        }
        if (selection8 !is KExpression<*>) {
            throw IllegalArgumentException("selection8 is not KExpression")
        }
        if (selection9 !is KExpression<*>) {
            throw IllegalArgumentException("selection9 is not KExpression")
        }
    }

    override fun precedence(): Int = 0
    
    @Suppress("UNCHECKED_CAST")
    override fun getType(): Class<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> =
        Tuple9::class.java as Class<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>>
    
    override fun accept(visitor: AstVisitor) {
        (selection1 as Ast).accept(visitor)
        (selection2 as Ast).accept(visitor)
        (selection3 as Ast).accept(visitor)
        (selection4 as Ast).accept(visitor)
        (selection5 as Ast).accept(visitor)
        (selection6 as Ast).accept(visitor)
        (selection7 as Ast).accept(visitor)
        (selection8 as Ast).accept(visitor)
        (selection9 as Ast).accept(visitor)
    }

    override fun renderTo(builder: SqlBuilder) {
        usingLowestPrecedence { 
            builder.sql("(")
            (selection1 as Ast).renderTo(builder)
            builder.sql(", ")
            (selection2 as Ast).renderTo(builder)
            builder.sql(", ")
            (selection3 as Ast).renderTo(builder)
            builder.sql(", ")
            (selection4 as Ast).renderTo(builder)
            builder.sql(", ")
            (selection5 as Ast).renderTo(builder)
            builder.sql(", ")
            (selection6 as Ast).renderTo(builder)
            builder.sql(", ")
            (selection7 as Ast).renderTo(builder)
            builder.sql(", ")
            (selection8 as Ast).renderTo(builder)
            builder.sql(", ")
            (selection9 as Ast).renderTo(builder)
            builder.sql(")")
        }
    }

    override fun size(): Int = 9

    override operator fun get(index: Int): Selection<*> =
        when (index) {
            0 -> selection1
            1 -> selection2
            2 -> selection3
            3 -> selection4
            4 -> selection5
            5 -> selection6
            6 -> selection7
            7 -> selection8
            8 -> selection9
            else -> throw IllegalArgumentException("index must between 0 and ${size() - 1}")
        }
}