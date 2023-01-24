export interface BookInput {
    
    readonly authorIds: ReadonlyArray<number>;
    
    readonly edition: number;
    
    readonly id?: number;
    
    readonly name: string;
    
    readonly price: number;
    
    readonly storeId?: number;
}