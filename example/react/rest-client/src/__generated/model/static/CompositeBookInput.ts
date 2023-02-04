import type { TargetOf_chapters } from './';

export interface CompositeBookInput {
    
    readonly authorIds: ReadonlyArray<number>;
    
    readonly chapters: ReadonlyArray<TargetOf_chapters>;
    
    readonly edition: number;
    
    readonly id?: number;
    
    readonly name: string;
    
    readonly price: number;
    
    readonly storeId?: number;
}