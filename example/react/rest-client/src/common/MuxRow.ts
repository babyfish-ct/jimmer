export type MuxRow<T> = 
    T extends object ?
    {-readonly [K in keyof T]: T[K]} :
    never;