export type Dynamic<T> = 
    {readonly [K in keyof T]?: Dynamic<T[K]>}
;
