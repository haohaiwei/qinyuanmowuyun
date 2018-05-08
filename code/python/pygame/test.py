########################################################
# hao,2018-02-06                                       #
# Detail:                                              #
# test for pygame                                      #
########################################################
# -*- coding: utf-8 -*-
import sys
import pygame
from pygame import *

'''def run_game():
    #pygame.init()
    screen=pygame.display.set_mode((1200,800))
    pygame.display.set_caption("test")
    bg_color=(230,230,230)
    screen.fill(bg_color)
    while True:
        for event in pygame.event.get():
            if event.type==pygame.QUIT:
                sys.exit()
                pygame.display.flip()                                          

run_game()'''
#from settings import Settings
'''def run_game():
    screen=pygame.display.set_mode((600,400))
    pygame.display.set_caption("Alien Invasion")
    bg_color = (230, 230, 230)

    while True:

        screen.fill(bg_color)

        pygame.display.flip()
run_game()'''
'''def run_game():
    pygame.init()
    ai_settings = Settings()
    screen = pygame.display.set_mode(
    (ai_settings.screen_width, ai_settings.screen_height))
    pygame.display.set_caption("Alien Invasion")

    while True:

        screen.fill(ai_settings.bg_color)

  
        pygame.display.flip()
run_game()'''
from settings import Settings
from ship import Ship
import game_functions as gf
from pygame.sprite import Group
def run_game():
    pygame.init()
    ai_settings = Settings()
    screen = pygame.display.set_mode(
        (ai_settings.screen_width, ai_settings.screen_height))
    pygame.display.set_caption("test")

    ship = Ship(ai_settings, screen)

    bullets = Group()

    while True:
        gf.check_events(ai_settings,screen,ship,bullets)
        ship.update()
        gf.update_bullets(bullets)
        gf.update_screen(ai_settings,screen, ship,bullets)
run_game()






























































































































































































































































































































































































































