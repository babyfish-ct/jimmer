export interface BookSpecification {
    readonly name: string | null | undefined;
    readonly minPrice: number | null | undefined;
    readonly maxPrice: number | null | undefined;
    readonly storeName: string | null | undefined;
    readonly authorName: string | null | undefined;
}
