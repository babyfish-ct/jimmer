import type { Book } from './';

export interface BookStore {
    
    readonly createdTime: string;
    
    readonly modifiedTime: string;
    
    readonly id: number;
    
    readonly name: string;
    
    readonly website?: string;
    
    readonly books: ReadonlyArray<Book>;
    
    readonly avgPrice: number;
    
    readonly newestBooks: ReadonlyArray<Book>;
}
