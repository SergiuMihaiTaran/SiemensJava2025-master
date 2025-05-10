package com.siemens.internship;
import static com.siemens.internship.Item.validEmail;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;

@ExtendWith(MockitoExtension.class)
class InternshipApplicationTests {

	@Mock
	private ItemRepository itemRepository;
	@Mock
	private ItemService itemService;
	@InjectMocks
	private ItemController itemController;
	@BeforeEach
	void setup() {
		itemService = new ItemService(itemRepository); // assuming constructor injection
		itemController = new ItemController(itemService);
	}
	@Test
	void testFindAll() {
		Item item1 = new Item(1L, "Item1", "cv", "status1", "cv@gmail.com");
		Item item2 = new Item(2L, "Item2", "cv2", "status1", "cv@yahoo.com");

		when(itemRepository.findAll()).thenReturn(Arrays.asList(item1, item2));

		ResponseEntity<List<Item>> expected = new ResponseEntity<>(Arrays.asList(item1, item2), HttpStatus.OK);
		assertEquals(expected, itemController.getAllItems());
		when(itemRepository.findAll()).thenReturn(List.of());
		 expected = new ResponseEntity<>(List.of(), HttpStatus.OK);
		assertEquals(expected, itemController.getAllItems());
	}
	@Test
	void testCreateItem() {
		Item validItem = new Item(1L, "Item1", "description", "status", "email@yahoo.com");

		BindingResult validResult = mock(BindingResult.class);
		when(validResult.hasErrors()).thenReturn(false);

		when(itemService.save(validItem)).thenReturn(validItem);

		ResponseEntity<Item> expected = new ResponseEntity<>(validItem, HttpStatus.CREATED);
		ResponseEntity<Item> actual = itemController.createItem(validItem, validResult);
		assertEquals(expected, actual);

		// invalid item scenario
		Item invalidItem = new Item();

		// mock binding result to have errors
		BindingResult invalidResult = mock(BindingResult.class);
		when(invalidResult.hasErrors()).thenReturn(true);

		ResponseEntity<Item> badRequestResponse = new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
		ResponseEntity<Item> actualBadRequest = itemController.createItem(invalidItem, invalidResult);
		assertEquals(badRequestResponse, actualBadRequest);
	}
	@Test
	void testGetItemById() {
		Long id = 1L;
		Item item = new Item(id, "Item1", "desc", "status", "email@yahoo.com");

		when(itemService.findById(id)).thenReturn(Optional.of(item));
		ResponseEntity<Item> expected = new ResponseEntity<>(item, HttpStatus.OK);
		ResponseEntity<Item> actual = itemController.getItemById(id);
		assertEquals(expected, actual);

		when(itemService.findById(id)).thenReturn(Optional.empty());
		ResponseEntity<Item> expectedNotFound = new ResponseEntity<>(HttpStatus.NOT_FOUND);
		ResponseEntity<Item> actualNotFound = itemController.getItemById(id);
		assertEquals(expectedNotFound, actualNotFound);
	}
	@Test
	void testUpdateItem() {
		Long id = 1L;
		Item updatedItem = new Item(id, "UpdatedItem", "updatedDesc", "updatedStatus", "updated@yahoo.com");
		//case 1:item exists
		when(itemService.findById(id)).thenReturn(Optional.of(new Item()));
		when(itemService.save(updatedItem)).thenReturn(updatedItem);

		ResponseEntity<Item> expectedResponse = new ResponseEntity<>(updatedItem, HttpStatus.OK);
		ResponseEntity<Item> actualResponse = itemController.updateItem(id, updatedItem);

		assertEquals(expectedResponse, actualResponse);
		assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
		assertEquals(updatedItem, actualResponse.getBody());
		//case 2:item does NOT exist
		when(itemService.findById(id)).thenReturn(Optional.empty());

		ResponseEntity<Item> notFoundResponse = itemController.updateItem(id, updatedItem);

		assertEquals(HttpStatus.NOT_FOUND, notFoundResponse.getStatusCode());
		assertNull(notFoundResponse.getBody());
	}
	@Test
	void testDeleteItem() {
		Long id = 1L;
		assertEquals(itemController.deleteItem(id).getStatusCode(),HttpStatus.NO_CONTENT);
	}
	@Test
	void testProcessItemsAsync() throws ExecutionException, InterruptedException, ExecutionException {
		List<Long> itemIds = Arrays.asList(1L, 2L, 3L);
		Item item1 = new Item(1L, "Item1", "desc", "status1", "email1@yahoo.com");
		Item item2 = new Item(2L, "Item2", "desc", "status1", "email2@yahoo.com");
		Item item3 = new Item(3L, "Item3", "desc", "status1", "email3@yahoo.com");

		when(itemRepository.findAllIds()).thenReturn(itemIds);

		when(itemRepository.findById(1L)).thenReturn(Optional.of(item1));
		when(itemRepository.findById(2L)).thenReturn(Optional.of(item2));
		when(itemRepository.findById(3L)).thenReturn(Optional.of(item3));

		when(itemRepository.save(any(Item.class))).thenReturn(item1).thenReturn(item2).thenReturn(item3);

		CompletableFuture<List<Item>> future = itemService.processItemsAsync();

		List<Item> processedItems = future.get(); // this blocks until all tasks are completed

		assertEquals(3, processedItems.size());
		assertEquals("PROCESSED", processedItems.get(0).getStatus());
		assertEquals("PROCESSED", processedItems.get(1).getStatus());
		assertEquals("PROCESSED", processedItems.get(2).getStatus());
	}
	@Test
	void testEmails() {
		// valid emails
		assertTrue(validEmail("test@example.com"));
		assertTrue(validEmail("user.name@example.com"));
		assertTrue(validEmail("plampe01@gmail.com"));
		// invalid emails
		assertFalse(validEmail(""));
		assertFalse(validEmail("plainaddress"));
		assertFalse(validEmail("missing@domain"));
		assertFalse(validEmail("@missinglocalpart.com"));
		assertFalse(validEmail("user@.domain.com"));
		assertFalse(validEmail("user@domain,com"));
		assertFalse(validEmail("user@domain.com@"));
		assertFalse(validEmail("user@domain..com"));
	}
}