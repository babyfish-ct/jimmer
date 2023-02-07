export interface CompositeBookInput {
    
    readonly authorIds: ReadonlyArray<number>;
    
    readonly chapters: ReadonlyArray<CompositeBookInput_TargetOf_chapters>;
    
    readonly edition: number;
    
    readonly id?: number;
    
    readonly name: string;
    
    readonly price: number;
    
    readonly storeId?: number;
}

export interface CompositeBookInput_TargetOf_chapters {
    
    readonly id?: number;
    
    readonly index: number;
    
    readonly title: string;
}
