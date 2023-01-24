export type ResponseOf<TFuncType> = 
    TFuncType extends (options: any) => Promise<infer TResponse> ? TResponse : never
;
