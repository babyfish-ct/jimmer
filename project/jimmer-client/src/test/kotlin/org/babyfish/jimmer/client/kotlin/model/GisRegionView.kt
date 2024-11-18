package org.babyfish.jimmer.client.kotlin.model

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Any
import kotlin.Boolean
import kotlin.Float
import kotlin.Int
import kotlin.String
import kotlin.Unit
import kotlin.collections.Iterable
import kotlin.collections.List
import kotlin.jvm.JvmStatic
import org.babyfish.jimmer.EmbeddableDto
import org.babyfish.jimmer.`internal`.GeneratedBy
import org.babyfish.jimmer.kt.new
import org.babyfish.jimmer.sql.fetcher.DtoMetadata
import org.babyfish.jimmer.sql.fetcher.`impl`.FetcherImpl

// Copy generated files into src like issue #789
@GeneratedBy(file = "<jimmer-client>/src/test/dto2/GisRegion.dto", prompt =
        "The current DTO type is immutable. If you need to make it mutable, please set the ksp argument `jimmer.dto.mutable` to the string \"text\"")
public open class GisRegionView(
    @get:JsonProperty(required = true)
    public val left: Float,
    @get:JsonProperty(required = true)
    public val top: Float,
    @get:JsonProperty(required = true)
    public val right: Float,
    @get:JsonProperty(required = true)
    public val bottom: Float,
) : EmbeddableDto<GisRegion> {
    public constructor(base: GisRegion) : this(
        base.left, 
        base.top, 
        base.right, 
        base.bottom)

    override fun toImmutable(): GisRegion = new(GisRegion::class).by(null,
            this@GisRegionView::toImmutableImpl)

    public fun toImmutable(block: GisRegionDraft.() -> Unit): GisRegion = new(GisRegion::class).by {
        toImmutableImpl(this)
        block(this)
    }

    /**
     * Avoid anonymous lambda affects coverage of non-kotlin-friendly tools such as jacoco
     */
    private fun toImmutableImpl(_draft: GisRegionDraft) {
        _draft.left = left
        _draft.top = top
        _draft.right = right
        _draft.bottom = bottom
    }

    public fun copy(
        left: Float = this.left,
        top: Float = this.top,
        right: Float = this.right,
        bottom: Float = this.bottom,
    ): GisRegionView = GisRegionView(left, top, right, bottom)

    public override fun hashCode(): Int {
        var _hash = left.hashCode()
        _hash = 31 * _hash + top.hashCode()
        _hash = 31 * _hash + right.hashCode()
        _hash = 31 * _hash + bottom.hashCode()
        return _hash
    }

    public override fun equals(o: Any?): Boolean {
        val _other = o as? GisRegionView ?: return false
        return left == _other.left &&
        top == _other.top &&
        right == _other.right &&
        bottom == _other.bottom
    }

    public override fun toString(): String = "GisRegionView(" +
        "left=" + left + 
        ", top=" + top + 
        ", right=" + right + 
        ", bottom=" + bottom + 
        ")"

    @GeneratedBy
    public companion object {
        @JvmStatic
        public val METADATA: DtoMetadata<GisRegion, GisRegionView> =
                    DtoMetadata<GisRegion, GisRegionView>(
                        // Use low level API to create fetcher 
                        // to avoid anonymous lambda that affects 
                        // coverage of non-kotlin-friendly tools
                        // such as jacoco
                        FetcherImpl(GisRegion::class.java)
                            .add("left")
                            .add("top")
                            .add("right")
                            .add("bottom"),
                        ::GisRegionView
                    )
    }
}

@GeneratedBy(type = GisRegion::class)
public fun Iterable<GisRegionView>.toImmutables(): List<GisRegion> = map(GisRegionView::toImmutable)

@GeneratedBy(type = GisRegion::class)
public fun Iterable<GisRegionView>.toImmutables(block: GisRegionDraft.() -> Unit): List<GisRegion> =
        map {
    it.toImmutable(block)
}
