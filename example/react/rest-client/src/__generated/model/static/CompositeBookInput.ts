export interface CompositeBookInput {
    
    readonly authorIds: ReadonlyArray<number>;
    
    readonly chapters: ReadonlyArray<string>;
    
    readonly edition: number;
    
    readonly id?: number;
    
    readonly name: string;
    
    readonly price: number;
    
    readonly storeId?: number;
}
