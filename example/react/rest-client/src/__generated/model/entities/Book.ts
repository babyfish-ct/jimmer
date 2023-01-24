import type { BookStore, Author, Chapter } from './';

export interface Book {
    
    readonly createdTime: string;
    
    readonly modifiedTime: string;
    
    readonly tenant: string;
    
    readonly id: number;
    
    readonly name: string;
    
    readonly edition: number;
    
    readonly price: number;
    
    readonly store?: BookStore;
    
    readonly authors: ReadonlyArray<Author>;
    
    readonly chapters: ReadonlyArray<Chapter>;
}