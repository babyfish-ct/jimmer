export interface BookInput {
    
    readonly authorIds: ReadonlyArray<number>;
    
    readonly createdTime: string;
    
    readonly edition: number;
    
    readonly id?: number;
    
    readonly modifiedTime: string;
    
    readonly name: string;
    
    readonly price: number;
    
    readonly storeId?: number;
    
    readonly tenant: string;
}