import type { Gender } from '../enums';
import type { Book } from './';

export interface Author {
    
    readonly createdTime: string;
    
    readonly modifiedTime: string;
    
    readonly id: number;
    
    readonly firstName: string;
    
    readonly lastName: string;
    
    readonly gender: Gender;
    
    readonly books: ReadonlyArray<Book>;
    
    readonly fullName: string;
}
