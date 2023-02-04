export type RequestOf<TFuncType> = 
    TFuncType extends (options: infer TRequest) => Promise<any> ? TRequest : never
;
