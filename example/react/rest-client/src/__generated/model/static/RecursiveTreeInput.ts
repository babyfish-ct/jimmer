export interface RecursiveTreeInput {
    
    readonly childNodes?: ReadonlyArray<RecursiveTreeInput_TargetOf_childNodes>;
    
    readonly id?: number;
    
    readonly name: string;
}

export interface RecursiveTreeInput_TargetOf_childNodes {
    
    readonly childNodes?: ReadonlyArray<RecursiveTreeInput_TargetOf_childNodes>;
    
    readonly id?: number;
    
    readonly name: string;
}
