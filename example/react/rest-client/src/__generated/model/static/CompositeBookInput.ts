export interface CompositeBookInput {
    
    readonly authorIds: ReadonlyArray<number>;
    
    readonly chapters: ReadonlyArray<CompositeBookInput_TargetOfChapters>;
    
    readonly edition: number;
    
    readonly id?: number;
    
    readonly name: string;
    
    readonly price: number;
    
    readonly storeId?: number;
}

export interface CompositeBookInput_TargetOfChapters {
    
    readonly index: number;
    
    readonly title: string;
}
