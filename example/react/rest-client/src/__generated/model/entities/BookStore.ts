import type { Book } from './';

export interface BookStore {
    
    readonly createdTime: string;
    
    readonly modifiedTime: string;
    
    readonly id: number;
    
    readonly name: string;
    
    readonly website?: string;
    
    readonly avgPrice: number;
    
    readonly books: ReadonlyArray<Book>;
}
