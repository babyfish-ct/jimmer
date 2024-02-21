export interface RecursiveTreeInput {
    readonly name: string;
    readonly childNodes?: ReadonlyArray<RecursiveTreeInput> | undefined;
}
