package org.babyfish.jimmer

import org.gradle.api.provider.ListProperty

abstract class DtoDirectories {
    abstract val main: ListProperty<String>
    abstract val test: ListProperty<String>
}
