export type Dynamic<T> = 
    {[K in keyof T]?: Dynamic<T[K]>}
;
