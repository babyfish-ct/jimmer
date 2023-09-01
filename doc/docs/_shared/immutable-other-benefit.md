-   Multi-thread safety, which is easy to understand and needs no explanation.

-   For collection containers that rely on hashCode, such as Set and Map, object immutability is desperately needed.

    Once mutable objects are held by hashCode sensitive collection containers like Set or Map *(as Key)*, developers must be very careful to ensure that the data shared by these containers is not modified. If a mistake is accidentally made, it usually takes debug tracking to find the problem, which often wastes time and affects efficiency. *(In fact, hashCode sensitive collection containers holding mutable objects is a common behavior, and it can also be said that most Java code is not strict, developers just avoid this problem.)*
  
-   In actual development, there are some other situations where objects are held for a long time. Although not dependent on hashCode, problems can also arise from holding objects for a long time, such as:

    - Using data persisted in WebSession for a long time
    
    - Using first-level cache, that is, using process-local cache in JVM memory to persist some data for a long time

    Careful developers certainly do not want references in WebSession or Cache that persist data for a long time to share data structures with references leaked to user code, which would lead to uncontrollable mutual interference.

    Therefore, when performing read/write operations on data structures persisted in WebSession or JVM internal Cache, careful developers will copy mutable data structures once before saving or returning them. Among them, copying when writing is still acceptable, but copying every time when reading is expensive. It can be seen that

    -   Using mutable objects, whether it is necessary to copy objects to ensure necessary security depends on the developer's ability to foresee risks. This requires developers to have some experience and be cautious by nature. However, even if the risks are foreseen, there is no objective standard for the solution. Being too strict will lead to too many unnecessary copies and waste, and being too loose will lead to insufficient copying and bugs *(the more team members, the easier to make mistakes)*. Moreover, for data of a certain volume, there are often disputes within the team about the strictness of this protection mechanism, which is highly subjective.
    
    -   Using immutable objects, the data structure is only copied in part *(here the "modification" is pseudo-modification, not real modification of the current data, which will be discussed in detail in subsequent documents)* when it is "modified" *(Jimmer/Immer internally optimizes this: the modified object will be copied, and from its parent object to the root node, all ancestor nodes will also be copied, while all other unchanged branches still share and reuse the original)* to get a new aggregate root reference, and simply share the original reference in all other cases. It has a very strict, indisputable objectivity.

    :::tip
    Undoubtedly, development based on objective laws is bound to be superior to development based on subjective feelings, which is very important.
    :::
