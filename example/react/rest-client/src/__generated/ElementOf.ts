export type ElementOf<T> = 
    T extends ReadonlyArray<infer TElement> ? TElement : never
;
